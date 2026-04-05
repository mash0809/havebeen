"use client";

import { useState, useEffect, useCallback, useRef, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import SimulationForm from "@/components/SimulationForm";
import GradeCard from "@/components/GradeCard";
import AnalogySummary from "@/components/AnalogySummary";
import SimulationChart from "@/components/SimulationChart";
import { fetchSimulation } from "@/lib/api";
import { SimulationRequest, SimulationResult } from "@/types/simulation";

/**
 * URL query string에서 시뮬레이션 파라미터를 파싱한다.
 * symbol, dailyAmount, startDate, endDate 모두 있어야 유효한 params로 반환.
 */
function parseSearchParams(
  searchParams: ReturnType<typeof useSearchParams>,
): SimulationRequest | undefined {
  const symbol = searchParams.get("symbol");
  const dailyAmountStr = searchParams.get("dailyAmount");
  const startDate = searchParams.get("startDate");
  const endDate = searchParams.get("endDate");

  if (!symbol || !dailyAmountStr || !startDate || !endDate) return undefined;

  const dailyAmount = Number(dailyAmountStr);
  if (!Number.isFinite(dailyAmount) || dailyAmount <= 0) return undefined;

  return { symbol, dailyAmount, startDate, endDate };
}

function HomeContent() {
  const searchParams = useSearchParams();

  const [result, setResult] = useState<SimulationResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // 링크 복사 버튼에서 현재 시뮬레이션 파라미터를 URL로 구성하기 위해 보관
  const [lastParams, setLastParams] = useState<SimulationRequest | null>(null);
  // 복사 성공 피드백 상태
  const [copySuccess, setCopySuccess] = useState(false);

  // URL query string에서 초기 파라미터 파싱
  const initialParams = parseSearchParams(searchParams);

  const handleSubmit = useCallback(async (params: SimulationRequest) => {
    setIsLoading(true);
    setError(null);
    setResult(null);
    // 링크 복사를 위해 마지막으로 실행된 파라미터 저장
    setLastParams(params);

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
  }, []); // fetchSimulation은 모듈 수준 함수라 deps 없음

  const hasAutoRun = useRef(false);

  // URL query string에 유효한 파라미터가 있으면 최초 1회 자동 시뮬레이션 실행
  useEffect(() => {
    if (!hasAutoRun.current && initialParams) {
      hasAutoRun.current = true;
      handleSubmit(initialParams);
    }
  }, [initialParams, handleSubmit]);

  const handleCopyLink = async () => {
    if (!lastParams) return;

    const url = new URL(window.location.origin);
    url.searchParams.set("symbol", lastParams.symbol);
    url.searchParams.set("dailyAmount", String(lastParams.dailyAmount));
    url.searchParams.set("startDate", lastParams.startDate);
    url.searchParams.set("endDate", lastParams.endDate);

    try {
      await navigator.clipboard.writeText(url.toString());
    } catch {
      // Clipboard API 미지원 환경: 사용자가 직접 복사할 수 있도록 URL 표시
      window.prompt("아래 링크를 복사하세요:", url.toString());
    }

    // 복사 완료 피드백: 2초 후 원복
    setCopySuccess(true);
    setTimeout(() => setCopySuccess(false), 2000);
  };

  return (
    <main className="min-h-screen px-4 py-10 bg-charcoal">
      {/* 헤더 */}
      <div className="text-center mb-10">
        <h1 className="text-4xl font-bold tracking-tight mb-2 text-gold">
          껄무새 계산기
        </h1>
        <p className="text-sm" style={{ color: "#888" }}>
          &ldquo;그때 살걸&rdquo; — 과거 적립식 투자 시뮬레이터
        </p>
      </div>

      {/* 입력 폼 — initialParams가 있으면 폼 초기값으로 주입 */}
      <SimulationForm
        onSubmit={handleSubmit}
        isLoading={isLoading}
        initialParams={initialParams}
      />

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
          {/* 링크 복사 버튼 — 현재 시뮬레이션 파라미터를 URL로 공유 */}
          <div className="w-full max-w-lg mx-auto flex justify-end">
            <button
              type="button"
              onClick={handleCopyLink}
              className="px-4 py-2 rounded-lg text-sm font-medium transition-all"
              style={{
                backgroundColor: "#0f3460",
                color: "#d4af37",
                border: "1px solid #2a2a4e",
              }}
            >
              {copySuccess ? "복사 완료!" : "링크 복사"}
            </button>
          </div>
          <GradeCard result={result} />
          <AnalogySummary result={result} />
          <SimulationChart result={result} />
        </div>
      )}
    </main>
  );
}

// useSearchParams()는 Suspense boundary 없이 사용하면 Next.js 15에서 빌드 경고가 발생한다.
// Home은 Suspense로 HomeContent를 감싸는 shell 역할만 한다.
export default function Home() {
  return (
    <Suspense fallback={null}>
      <HomeContent />
    </Suspense>
  );
}
