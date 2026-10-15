from dataclasses import dataclass
from pathlib import PurePosixPath
import re


@dataclass(frozen=True)
class PatchValidationResult:
    valid: bool
    reason: str


def validate_patch(
    patch: str,
    expected_file_path: str,
) -> PatchValidationResult:
    patch = patch.strip()
    expected_file_path = expected_file_path.strip()

    if not patch:
        return PatchValidationResult(
            valid=False,
            reason="Patch is empty.",
        )

    if not expected_file_path:
        return PatchValidationResult(
            valid=False,
            reason="Expected file path is empty.",
        )

    if "```" in patch:
        return PatchValidationResult(
            valid=False,
            reason="Patch must not contain Markdown code fences.",
        )

    lines = patch.splitlines()

    if len(lines) < 3:
        return PatchValidationResult(
            valid=False,
            reason="Patch is too short to be a unified diff.",
        )

    if not lines[0].startswith("--- "):
        return PatchValidationResult(
            valid=False,
            reason="Patch is missing the --- file header.",
        )

    if not lines[1].startswith("+++ "):
        return PatchValidationResult(
            valid=False,
            reason="Patch is missing the +++ file header.",
        )

    if not any(line.startswith("@@ ") for line in lines):
        return PatchValidationResult(
            valid=False,
            reason="Patch is missing a unified diff hunk header.",
        )

    old_path = _extract_diff_path(lines[0][4:])
    new_path = _extract_diff_path(lines[1][4:])

    if old_path != expected_file_path:
        return PatchValidationResult(
            valid=False,
            reason=(
                f"Patch references unexpected source path '{old_path}'; "
                f"expected '{expected_file_path}'."
            ),
        )

    if new_path != expected_file_path:
        return PatchValidationResult(
            valid=False,
            reason=(
                f"Patch references unexpected target path '{new_path}'; "
                f"expected '{expected_file_path}'."
            ),
        )

    if _is_unsafe_path(old_path) or _is_unsafe_path(new_path):
        return PatchValidationResult(
            valid=False,
            reason="Patch contains an unsafe file path.",
        )

    changed_lines = [
        line
        for line in lines[2:]
        if line.startswith("+") or line.startswith("-")
    ]

    if not changed_lines:
        return PatchValidationResult(
            valid=False,
            reason="Patch contains no added or removed lines.",
        )

    return PatchValidationResult(
        valid=True,
        reason="Patch passed structural validation.",
    )


def _extract_diff_path(header: str) -> str:
    path = header.split("\t", 1)[0].strip()

    if path.startswith("a/") or path.startswith("b/"):
        path = path[2:]

    return path


def _is_unsafe_path(path: str) -> bool:
    normalized = PurePosixPath(path)

    if normalized.is_absolute():
        return True

    if ".." in normalized.parts:
        return True

    if re.match(r"^[A-Za-z]:", path):
        return True

    return False
