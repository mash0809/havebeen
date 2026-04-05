export interface SimulationRequest {
  symbol: string;
  dailyAmount: number;
  startDate: string;
  endDate: string;
}

export interface ChartPoint {
  date: string;
  value: number;
}

export interface SimulationResult {
  symbol: string;
  startDate: string;
  endDate: string;
  dailyAmount: number;
  totalInvested: number;
  currentValue: number;
  returnRate: number;
  grade: string;
  analogyText: string;
  chartData: ChartPoint[];
}

export interface StockSearchResult {
  symbol: string;
  name: string;
  exchange: string;
}
