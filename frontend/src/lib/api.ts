import { SimulationRequest, SimulationResult } from "@/types/simulation";

const BASE_URL = "http://localhost:8080";

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
