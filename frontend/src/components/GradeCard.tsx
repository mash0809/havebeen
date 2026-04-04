import { SimulationResult } from "@/types/simulation";

interface GradeCardProps {
  result: SimulationResult;
}

function formatKRW(value: number): string {
  // 1억 이상이면 억 단위로 표시, 아니면 만 단위
  if (Math.abs(value) >= 100_000_000) {
    return `${(value / 100_000_000).toFixed(2)}억원`;
  }
  if (Math.abs(value) >= 10_000) {
    return `${(value / 10_000).toFixed(1)}만원`;
  }
  return `${value.toLocaleString()}원`;
}

export default function GradeCard({ result }: GradeCardProps) {
  const isPositive = result.returnRate >= 0;
  const returnColorClass = isPositive ? "text-rise" : "text-fall";
  const returnPrefix = isPositive ? "+" : "";

  return (
    <div
      className="w-full max-w-lg mx-auto p-6 rounded-2xl"
      style={{ backgroundColor: "#16213e" }}
    >
      {/* 껄무새 등급 */}
      <div className="text-center mb-4">
        <p className="text-sm mb-1" style={{ color: "#888" }}>
          껄무새 등급
        </p>
        <p className="text-3xl font-bold text-gold">
          {result.grade}
        </p>
      </div>

      {/* 수익률 */}
      <div className="text-center mb-6">
        <p className={`text-5xl font-bold ${returnColorClass}`}>
          {returnPrefix}{result.returnRate.toFixed(2)}%
        </p>
      </div>

      {/* 투자금 / 평가금 */}
      <div
        className="flex justify-around pt-4"
        style={{ borderTop: "1px solid #2a2a4e" }}
      >
        <div className="text-center">
          <p className="text-xs mb-1" style={{ color: "#888" }}>
            총 투자금
          </p>
          <p className="text-lg font-semibold" style={{ color: "#ffffff" }}>
            {formatKRW(result.totalInvested)}
          </p>
        </div>
        <div
          className="w-px"
          style={{ backgroundColor: "#2a2a4e" }}
        />
        <div className="text-center">
          <p className="text-xs mb-1" style={{ color: "#888" }}>
            평가금액
          </p>
          <p className={`text-lg font-semibold ${returnColorClass}`}>
            {formatKRW(result.currentValue)}
          </p>
        </div>
      </div>
    </div>
  );
}
