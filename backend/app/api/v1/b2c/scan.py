from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import verify_firebase_token
from app.schemas.b2c import ScanAnalyzeRequest, ScanAnalyzeResponse

router = APIRouter()


@router.post("/analyze", response_model=ScanAnalyzeResponse)
async def analyze_scan(
    request: ScanAnalyzeRequest,
    token: dict = Depends(verify_firebase_token),
    db: AsyncSession = Depends(get_db),
) -> ScanAnalyzeResponse:
    """
    Main B2C endpoint. Analyzes OBD scan from ELM327 adapter.

    Flow:
    1. Rule Engine → severity, can_drive (deterministic, no LLM)
    2. RAG Retrieval → context from vector DB (premium only)
    3. LLM Router → local Llama or Claude API
    4. Output Formatter → structured JSON for mobile app
    """
    # TODO: implement AI pipeline
    raise NotImplementedError
