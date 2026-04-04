import { SimulationResult } from "@/types/simulation";

interface AnalogySummaryProps {
  result: SimulationResult;
}

function formatDateRange(startDate: string, endDate: string): string {
  // 두 날짜 사이의 개월 수를 계산
  const start = new Date(startDate);
  const end = new Date(endDate);
  const months =
    (end.getFullYear() - start.getFullYear()) * 12 +
    (end.getMonth() - start.getMonth());

  if (months >= 12) {
    const years = Math.floor(months / 12);
    const remainingMonths = months % 12;
    return remainingMonths > 0
      ? `${years}년 ${remainingMonths}개월`
      : `${years}년`;
  }
  return `${months}개월`;
}

export default function AnalogySummary({ result }: AnalogySummaryProps) {
  const period = formatDateRange(result.startDate, result.endDate);

  return (
    <div
      className="w-full max-w-lg mx-auto p-6 rounded-2xl"
      style={{ backgroundColor: "#16213e" }}
    >
      {/* 비유 텍스트 */}
      <div className="text-center mb-4">
        <p className="text-sm mb-2" style={{ color: "#888" }}>
          그 돈으로 살 수 있었던 것
        </p>
        <p className="text-2xl font-bold leading-snug text-gold">
          {result.analogyText}
        </p>
      </div>

      {/* 투자 정보 요약 */}
      <div
        className="flex justify-around pt-4"
        style={{ borderTop: "1px solid #2a2a4e" }}
      >
        <div className="text-center">
          <p className="text-xs mb-1" style={{ color: "#888" }}>
            투자 기간
          </p>
          <p className="text-base font-semibold" style={{ color: "#ffffff" }}>
            {period}
          </p>
          <p className="text-xs mt-0.5" style={{ color: "#555" }}>
            {result.startDate} ~ {result.endDate}
          </p>
        </div>
        <div className="w-px" style={{ backgroundColor: "#2a2a4e" }} />
        <div className="text-center">
          <p className="text-xs mb-1" style={{ color: "#888" }}>
            일일 투자금
          </p>
          <p className="text-base font-semibold" style={{ color: "#ffffff" }}>
            {result.dailyAmount.toLocaleString()}원
          </p>
        </div>
      </div>
    </div>
  );
}
