from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import verify_firebase_token
from app.schemas.b2b import DiagnosticAnalyzeRequest, DiagnosticAnalyzeResponse

router = APIRouter()


@router.post("/analyze", response_model=DiagnosticAnalyzeResponse)
async def analyze_diagnostic(
    request: DiagnosticAnalyzeRequest,
    token: dict = Depends(verify_firebase_token),
    db: AsyncSession = Depends(get_db),
) -> DiagnosticAnalyzeResponse:
    """
    Main B2B endpoint. Professional diagnostic analysis for mechanics.

    Input: DTC codes + freeze frame + symptoms + vehicle info
    Output: probable causes ranked by %, diagnostic checklist, labor estimate

    Always routes to external API (Claude) for maximum quality.
    """
    # TODO: implement AI pipeline (B2B always uses Claude API)
    raise NotImplementedError
