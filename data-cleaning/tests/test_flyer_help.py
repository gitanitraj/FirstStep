"""IM-04 exact-token and executable catalog-gate evidence."""
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path('data-cleaning/scripts/validate_schema.py')
SPEC = importlib.util.spec_from_file_location('flyer_validator', SCRIPT)
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)


class FlyerHelpTests(unittest.TestCase):
    def test_absence_and_exact_value_are_valid(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'flyers.json'
            path.write_text(json.dumps({'records': [{'id': 'ordinary'}, {'id': 'help', 'helpAccess': 'ACTIONABLE_HELP_ACCESS'}]}))
            self.assertEqual(0, validator.validate_flyer_help_file(path))

    def test_unknown_and_malformed_values_fail_exact_diagnostic(self):
        for value in [None, '', 'actionable_help_access', ' ACTIONABLE_HELP_ACCESS', 'UNKNOWN', True, 1, [], {}, ['ACTIONABLE_HELP_ACCESS']]:
            with self.subTest(value=value), tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / 'flyers.json'
                path.write_text(json.dumps({'records': [{'id': 'SYN-invalid', 'helpAccess': value}]}))
                result = subprocess.run([sys.executable, str(SCRIPT), '--flyer-help-only', '--flyer-input', str(path)], capture_output=True, text=True)
                self.assertEqual(1, result.returncode)
                self.assertIn('SYN-invalid: invalid_help_access:', result.stdout)

