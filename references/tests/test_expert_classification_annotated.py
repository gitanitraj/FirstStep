# IM-03 complete annotated mirror of data-cleaning/tests/test_expert_classification.py
# Existing validator gate extended for explicit Expert/FAQ relationships; no inference.
# Focused tests assert precise diagnostics, fixture exits, real records and preservation.
"""IM-03: exercise the existing schema gate and its exact diagnostic paths."""
import copy
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("validate_schema", ROOT / "data-cleaning/scripts/validate_schema.py")
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)
CATEGORIES = json.loads((ROOT / "app/data/taxonomy.json").read_text())["categories"]


class ExpertClassificationTests(unittest.TestCase):
    def errors(self, **fields):
        return validator.validate_expert_record({"id": "TEST-RECORD", **fields}, CATEGORIES)

    def test_category_only_and_distinct_multi_category_are_valid(self):
        for tags in (["Housing"], ["Housing", "Legal"], ["housing"]):
            with self.subTest(tags=tags):
                self.assertEqual([], self.errors(category_tags=tags))

    def test_missing_empty_and_malformed_category_are_identified(self):
        for value in (None, [], "Housing"):
            with self.subTest(value=value):
                self.assertTrue(self.errors(category_tags=value)[0].startswith("missing_category:"))
        for value in ([None], [""], [42]):
            with self.subTest(value=value):
                self.assertTrue(self.errors(category_tags=value)[0].startswith("malformed_category:"))

    def test_unknown_category_has_specific_diagnostic(self):
        self.assertEqual(["unknown_category: Invented"], self.errors(category_tags=["Housing", "Invented"]))

    def test_topic_without_parent_cannot_manufacture_category(self):
        errors = self.errors(subcategory="Eviction Prevention")
        self.assertTrue(any(e.startswith("missing_category:") for e in errors))
        self.assertTrue(any(e.startswith("parent_mismatch:") for e in errors))

    def test_valid_topic_requires_an_explicit_parent_not_every_multi_category(self):
        self.assertEqual([], self.errors(category_tags=["Housing", "Food"], subcategory="Eviction Prevention"))
        self.assertEqual([], self.errors(category_tags=["Housing"], subcategory="Eviction Prevention"))
        self.assertEqual([], self.errors(category_tags=["Housing"], subcategory=""))

    def test_mismatched_and_unknown_topic_are_distinct_errors(self):
        self.assertTrue(self.errors(category_tags=["Food"], subcategory="Eviction Prevention")[0].startswith("parent_mismatch:"))
        self.assertTrue(self.errors(category_tags=["Housing"], subcategory="Invented Topic")[0].startswith("invalid_topic:"))
        self.assertTrue(self.errors(category_tags=["Housing"], subcategory=42)[0].startswith("invalid_topic:"))

    def test_duplicate_case_variants_are_rejected(self):
        self.assertEqual(["duplicate_category: housing"], self.errors(category_tags=["Housing", "housing"]))

    def test_inactive_expired_or_draft_metadata_does_not_hide_classification_defects(self):
        for context in ({"status": "inactive"}, {"status": "withdrawn"}, {"status": "draft"}, {"expirationDate": "2000-01-01"}):
            with self.subTest(context=context):
                self.assertTrue(self.errors(**context)[0].startswith("missing_category:"))

    def test_classification_is_independent_of_community_and_does_not_mutate(self):
        record = {"id": "SECOND-COMMUNITY", "communityId": "test-community", "category_tags": ["Housing"], "subcategory": "Eviction Prevention"}
        before = copy.deepcopy(record)
        self.assertEqual([], validator.validate_expert_record(record, CATEGORIES))
        self.assertEqual(before, record)

    def test_real_catalog_has_the_approved_explicit_assignments_without_topics(self):
        expected = {"FAQ-001": "Food", "FAQ-002": "Housing", "FAQ-003": "Legal", "FAQ-004": "Utilities", "FAQ-005": "Health", "FAQ-006": "Employment", "EA-001": "Housing", "EA-002": "Food", "EA-003": "Employment", "EA-004": "Health", "EA-005": "Housing", "EA-006": "Utilities"}
        found = set()
        for file in ("faq.json", "expert-answers.json"):
            for record in json.loads((ROOT / "app/data" / file).read_text())["records"]:
                self.assertEqual([], validator.validate_expert_record(record, CATEGORIES), record["id"])
                if record["id"] in expected:
                    found.add(record["id"])
                    self.assertEqual([expected[record["id"]]], record["category_tags"])
                    self.assertIsNone(record.get("subcategory"))
        self.assertEqual(set(expected), found)

    def test_real_cli_rejects_injected_missing_category_and_restored_input_passes(self):
        with tempfile.TemporaryDirectory(prefix="im03-validator-") as folder:
            file = Path(folder) / "faq.json"
            record = {"id": "IM03-NEGATIVE", "communityId": "second-community", "subcategory": "Eviction Prevention"}
            command = [sys.executable, str(ROOT / "data-cleaning/scripts/validate_schema.py"), "--expert-only", "--expert-input", str(file)]
            file.write_text(json.dumps({"records": [record]}))
            invalid = subprocess.run(command, cwd=ROOT, capture_output=True, text=True)
            self.assertEqual(1, invalid.returncode)
            self.assertIn("IM03-NEGATIVE", invalid.stdout)
            self.assertIn("missing_category:", invalid.stdout)
            self.assertIn("parent_mismatch:", invalid.stdout)
            record["category_tags"] = ["Housing"]
            file.write_text(json.dumps({"records": [record]}))
            valid = subprocess.run(command, cwd=ROOT, capture_output=True, text=True)
            self.assertEqual(0, valid.returncode, valid.stdout)
            self.assertIn("classification failures: 0", valid.stdout)
            again = subprocess.run(command, cwd=ROOT, capture_output=True, text=True)
            self.assertEqual(valid.stdout, again.stdout)

    def test_missing_file_is_a_catalog_error_not_a_zero_record_success(self):
        result = subprocess.run([sys.executable, str(ROOT / "data-cleaning/scripts/validate_schema.py"), "--expert-only", "--expert-input", "/tmp/im03-nonexistent-catalog-file.json"], cwd=ROOT, capture_output=True, text=True)
        self.assertEqual(1, result.returncode)
        self.assertIn("catalog_read_error:", result.stdout)


if __name__ == "__main__":
    unittest.main()
