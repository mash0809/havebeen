import { SimulationRequest, SimulationResult, StockSearchResult } from "@/types/simulation";

// 배포 환경에서는 NEXT_PUBLIC_API_BASE_URL 환경변수로 백엔드 주소를 지정한다
const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export async function fetchSimulation(
  params: SimulationRequest,
): Promise<SimulationResult> {
  const query = new URLSearchParams({
    symbol: params.symbol,
    dailyAmount: String(params.dailyAmount),
    startDate: params.startDate,
    endDate: params.endDate,
  });

  const response = await fetch(`${BASE_URL}/api/simulation?${query}`);

  if (!response.ok) {
    throw new Error(`API 오류: ${response.status} ${response.statusText}`);
  }

  return response.json();
}

export async function searchStocks(query: string): Promise<StockSearchResult[]> {
  if (query.length < 2) return [];
  const response = await fetch(`${BASE_URL}/api/stocks/search?q=${encodeURIComponent(query)}`);
  if (!response.ok) return [];
  return response.json();
}
