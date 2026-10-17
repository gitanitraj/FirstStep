# Annotated reference: data-cleaning/tests/test_im12_organization_references.py
# WHAT: Executes named regression cases for the identity, Community, provenance or resident-public contract.
# WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
# INTERACTIONS: Uses actual stored repositories or isolated synthetic fixtures, never mutating production catalogs or invoking remote providers.
# ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
# BEGIN MIRRORED SOURCE
"""Optional supplementary provider IDs are checked, never guessed from names."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("im12_org_validator", Path(__file__).parents[1] / "scripts/validate_organizations.py")
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)

class Im12OrganizationReferences(unittest.TestCase):
    def validate(self, reference):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            registry = root / "organizations.json"
            registry.write_text(json.dumps([{"id":"ORG-001","name":"Real","slug":"real"}]))
            supplementary = root / "resources.communities.json"
            supplementary.write_text(json.dumps([{"id":"synthetic", "organization":"Real", **reference}]))
            output = io.StringIO()
            with patch.object(validator, "REGISTRY", registry), patch.object(validator, "PROVIDER_SOURCES", []), patch.object(validator, "OPTIONAL_REFERENCE_SOURCES", [(supplementary,"resources.communities","organization")], create=True), patch("sys.argv", ["validate"]), contextlib.redirect_stdout(output):
                result = validator.main()
            return result, output.getvalue()

    def test_im12_t03_explicit_unknown_supplementary_reference_is_rejected(self):
        result, output = self.validate({"organization_id":"ORG-999"})
        self.assertEqual(1, result)
        self.assertIn("unknown organization_id 'ORG-999'", output)

    def test_im12_t05_missing_optional_reference_does_not_invent_identity(self):
        result, output = self.validate({})
        self.assertEqual(0, result)
        self.assertNotIn("ERROR", output)
