import importlib.util
import tempfile
import unittest
from pathlib import Path

from PIL import Image


MODULE_PATH = Path(__file__).with_name("generate_figure1_architecture.py")


class FigureArchitectureGenerationTest(unittest.TestCase):
    def _load_module(self):
        if not MODULE_PATH.exists():
            self.fail(f"Missing generator script: {MODULE_PATH}")

        spec = importlib.util.spec_from_file_location(
            "generate_figure1_architecture", MODULE_PATH
        )
        module = importlib.util.module_from_spec(spec)
        assert spec.loader is not None
        spec.loader.exec_module(module)
        return module

    def test_generates_png_with_expected_size_and_palette(self):
        module = self._load_module()

        with tempfile.TemporaryDirectory() as tmp_dir:
            output_path = Path(tmp_dir) / "figure1_architecture.png"
            module.render_architecture(output_path)

            self.assertTrue(output_path.exists(), "PNG output file was not created")

            with Image.open(output_path) as image:
                self.assertEqual(image.size, (1800, 1000))
                colors = {
                    color[:3] if isinstance(color, tuple) and len(color) == 4 else color
                    for color in image.getdata()
                }

            self.assertIn((217, 225, 242), colors, "Missing NL layer fill color")
            self.assertIn((226, 239, 218), colors, "Missing causal layer fill color")


if __name__ == "__main__":
    unittest.main()
