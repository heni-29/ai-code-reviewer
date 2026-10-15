from langgraph.graph import StateGraph, START, END

from app.graph.state import AgentState
from app.graph.nodes import (
    plan_investigation,
    retrieve_context,
    diagnose_issue,
    propose_patch,
    verify_patch,
)


builder = StateGraph(AgentState)

# Register the workflow nodes
builder.add_node("plan", plan_investigation)
builder.add_node("retrieve", retrieve_context)
builder.add_node("diagnose", diagnose_issue)
builder.add_node("propose_patch", propose_patch)
builder.add_node("verify", verify_patch)

# Define execution order
builder.add_edge(START, "plan")
builder.add_edge("plan", "retrieve")
builder.add_edge("retrieve", "diagnose")
builder.add_edge("diagnose", "propose_patch")
builder.add_edge("propose_patch", "verify")
builder.add_edge("verify", END)

# Compile the workflow
graph = builder.compile()