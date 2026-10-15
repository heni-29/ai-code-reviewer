from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from app.graph.workflow import graph


app = FastAPI(
    title="Autonomous AI Software Engineer",
    description="Investigates software bugs using a LangGraph workflow.",
    version="0.1.0",
)


class InvestigationRequest(BaseModel):
    repository: str = Field(min_length=1)
    issue: str = Field(min_length=1)
    branch: str = "main"
    context: str = ""
    file_path: str = ""
    use_github: bool = False


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/investigations")
def investigate(request: InvestigationRequest):
    try:
        result = graph.invoke(
            {
                "repository": request.repository,
                "issue": request.issue,
                "branch": request.branch,
                "supplied_context": request.context,
                "file_path": request.file_path,
                "use_github": request.use_github,
            }
        )

        return {
            "repository": request.repository,
            "branch": request.branch,
            "plan": result.get("plan", ""),
            "diagnosis": result.get("diagnosis", ""),
            "proposed_patch": result.get("proposed_patch", ""),
            "verification": result.get("verification", ""),
            "status": result.get("status", "unknown"),
        }

    except Exception as exc:
        raise HTTPException(
            status_code=503,
            detail="Investigation failed. Check agent and Ollama logs.",
        ) from exc