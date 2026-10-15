import os

from github import Auth, Github


def get_github_client() -> Github:
    token = os.getenv("GITHUB_TOKEN")

    if token:
        return Github(auth=Auth.Token(token))

    return Github()


def get_repository_file(
    repository: str,
    file_path: str,
    ref: str = "main",
) -> str:
    client = get_github_client()

    try:
        repo = client.get_repo(repository)
        content = repo.get_contents(file_path, ref=ref)

        if isinstance(content, list):
            raise ValueError(
                "The requested path is a directory, not a file."
            )

        if content.encoding != "base64":
            raise ValueError(
                "Unsupported GitHub file encoding."
            )

        return content.decoded_content.decode("utf-8")

    finally:
        client.close()

def get_repository_tree(
    repository: str,
    ref: str = "main",
) -> list[str]:
    client = get_github_client()

    try:
        repo = client.get_repo(repository)
        tree = repo.get_git_tree(ref, recursive=True)

        return [
            item.path
            for item in tree.tree
            if item.type == "blob"
        ]

    finally:
        client.close()

def get_repository_commit_sha(repository: str, ref: str = "main") -> str:
    client = get_github_client()

    try:
        repo = client.get_repo(repository)
        branch = repo.get_branch(ref)
        return branch.commit.sha
    finally:
        client.close()
