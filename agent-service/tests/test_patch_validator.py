from unittest import result

from app.graph.patch_validator import validate_patch


def test_valid_patch():
    patch = """--- calculate.py
+++ calculate.py
@@ -1,2 +1,2 @@
 def calculate(a, b):
-    return a - b
+    return a + b
"""

    result = validate_patch(
        patch=patch,
        expected_file_path="calculate.py",
    )

    assert result.valid is True


def test_rejects_wrong_file_path():
    patch = """--- calculate_total.py
+++ calculate_total.py
@@ -1,2 +1,2 @@
 def calculate(a, b):
-    return a - b
+    return a + b
"""

    result = validate_patch(
        patch=patch,
        expected_file_path="calculate.py",
    )

    assert result.valid is False
    assert "unexpected" in result.reason


def test_rejects_path_traversal():
    patch = """--- ../../secret.py
+++ ../../secret.py
@@ -1 +1 @@
-old
+new
"""

    result = validate_patch(
        patch=patch,
        expected_file_path="../../secret.py",
    )

    assert result.valid is False
    assert "unsafe" in result.reason


def test_rejects_markdown_code_fence():
    patch = """```diff
--- calculate.py
+++ calculate.py
@@ -1 +1 @@
-old
+new
```"""

    result = validate_patch(
        patch=patch,
        expected_file_path="calculate.py",
    )

    assert result.valid is False
    assert "code fences" in result.reason


def test_rejects_missing_hunk():
    patch = """--- calculate.py
+++ calculate.py
"""

    result = validate_patch(
        patch=patch,
        expected_file_path="calculate.py",
    )

    assert result.valid is False
    assert "unified diff" in result.reason