"use client";

import { useState } from "react";
import SimulationForm from "@/components/SimulationForm";
import GradeCard from "@/components/GradeCard";
import AnalogySummary from "@/components/AnalogySummary";
import SimulationChart from "@/components/SimulationChart";
import { fetchSimulation } from "@/lib/api";
import { SimulationRequest, SimulationResult } from "@/types/simulation";

export default function Home() {
  const [result, setResult] = useState<SimulationResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (params: SimulationRequest) => {
    setIsLoading(true);
    setError(null);
    setResult(null);

    try {
      const data = await fetchSimulation(params);
      setResult(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "알 수 없는 오류가 발생했습니다.",
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <main className="min-h-screen px-4 py-10 bg-charcoal">
      {/* 헤더 */}
      <div className="text-center mb-10">
        <h1
          className="text-4xl font-bold tracking-tight mb-2 text-gold"
        >
          껄무새 계산기
        </h1>
        <p className="text-sm" style={{ color: "#888" }}>
          &ldquo;그때 살걸&rdquo; — 과거 적립식 투자 시뮬레이터
        </p>
      </div>

      {/* 입력 폼 */}
      <SimulationForm onSubmit={handleSubmit} isLoading={isLoading} />

      {/* 에러 메시지 */}
      {error && (
        <div
          role="alert"
          className="w-full max-w-lg mx-auto mt-4 p-4 rounded-xl text-sm text-rise"
          style={{ backgroundColor: "#2a1a1a", border: "1px solid #EF4444" }}
        >
          {error}
        </div>
      )}

      {/* 결과 없을 때 안내 문구 */}
      {!result && !isLoading && !error && (
        <div className="text-center mt-12">
          <p className="text-base" style={{ color: "#444" }}>
            종목과 기간을 설정하고 시뮬레이션을 시작해보세요.
          </p>
        </div>
      )}

      {/* 결과 섹션 */}
      {result && (
        <div className="mt-8 flex flex-col gap-4">
          <GradeCard result={result} />
          <AnalogySummary result={result} />
          <SimulationChart result={result} />
        </div>
      )}
    </main>
  );
}
