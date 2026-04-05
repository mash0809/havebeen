"use client";

import { useState, useEffect, useRef } from "react";
import { SimulationRequest, StockSearchResult } from "@/types/simulation";
import { STOCK_PRESETS } from "@/lib/presets";
import { searchStocks } from "@/lib/api";

interface SimulationFormProps {
  onSubmit: (params: SimulationRequest) => void;
  isLoading: boolean;
  // URL query string 등으로 초기값을 주입할 때 사용
  initialParams?: SimulationRequest;
}

function getDefaultDates(): { startDate: string; endDate: string } {
  const today = new Date();
  // toISOString()은 UTC 기준이므로 한국 시간대에서 어제 날짜가 나올 수 있다.
  // 로컬 날짜를 직접 포맷해서 타임존 버그를 방지한다.
  const formatDate = (d: Date) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

  const endDate = formatDate(today);
  const oneYearAgo = new Date(today);
  oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
  const startDate = formatDate(oneYearAgo);

  return { startDate, endDate };
}

/** initialParams의 symbol이 프리셋 목록과 일치하는지 찾아 index를 반환한다. */
function resolvePresetIndex(symbol?: string): number {
  if (!symbol) return 0;
  const idx = STOCK_PRESETS.findIndex((p) => p.symbol === symbol);
  // 일치하는 프리셋이 없으면 "직접 입력"(마지막 index)로 설정
  return idx !== -1 ? idx : STOCK_PRESETS.length - 1;
}

export default function SimulationForm({
  onSubmit,
  isLoading,
  initialParams,
}: SimulationFormProps) {
  const { startDate: defaultStart, endDate: defaultEnd } = getDefaultDates();

  const [selectedPresetIndex, setSelectedPresetIndex] = useState(
    resolvePresetIndex(initialParams?.symbol),
  );
  const [customSymbol, setCustomSymbol] = useState(() => {
    // initialParams.symbol이 있고 프리셋과 일치하지 않으면 직접 입력값으로 사용
    if (!initialParams?.symbol) return "";
    const idx = STOCK_PRESETS.findIndex((p) => p.symbol === initialParams.symbol);
    return idx === -1 ? initialParams.symbol : "";
  });
  const [dailyAmount, setDailyAmount] = useState(initialParams?.dailyAmount ?? 10000);
  const [startDate, setStartDate] = useState(initialParams?.startDate ?? defaultStart);
  const [endDate, setEndDate] = useState(initialParams?.endDate ?? defaultEnd);
  const [formError, setFormError] = useState<string | null>(null);

  // 검색 관련 state
  const [searchQuery, setSearchQuery] = useState(() => {
    // initialParams.symbol이 프리셋에 없으면 직접 입력 모드로 초기화
    if (!initialParams?.symbol) return "";
    const idx = STOCK_PRESETS.findIndex((p) => p.symbol === initialParams.symbol);
    return idx === -1 ? initialParams.symbol : "";
  });
  const [searchResults, setSearchResults] = useState<StockSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const isCustom = selectedPresetIndex === STOCK_PRESETS.length - 1;

  // debounce 검색: searchQuery 변경 시 300ms 후 API 호출
  useEffect(() => {
    if (!isCustom) return;

    if (searchQuery.length < 2) {
      setSearchResults([]);
      setShowDropdown(false);
      return;
    }

    setIsSearching(true);
    const timer = setTimeout(async () => {
      const results = await searchStocks(searchQuery);
      setSearchResults(results);
      setIsSearching(false);
      setShowDropdown(results.length > 0);
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery, isCustom]);

  // 드롭다운 외부 클릭 시 닫기
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelectStock = (result: StockSearchResult) => {
    // 선택한 종목의 symbol을 customSymbol에 저장하고 검색 텍스트는 name으로 표시
    setCustomSymbol(result.symbol);
    setSearchQuery(result.name);
    setShowDropdown(false);
  };

  const handleSearchQueryChange = (value: string) => {
    setSearchQuery(value);
    // 검색어가 바뀌면 이전에 선택한 symbol 초기화 (직접 입력 허용)
    setCustomSymbol("");
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // customSymbol이 비어있으면 searchQuery를 심볼로 사용 (직접 입력 허용)
    const symbol = isCustom
      ? (customSymbol || searchQuery.trim())
      : STOCK_PRESETS[selectedPresetIndex].symbol;

    if (!symbol) {
      setFormError("종목 코드를 입력해주세요.");
      return;
    }

    if (startDate >= endDate) {
      setFormError("시작일은 종료일보다 이전이어야 합니다.");
      return;
    }
    setFormError(null);

    onSubmit({ symbol, dailyAmount, startDate, endDate });
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="w-full max-w-lg mx-auto p-6 rounded-2xl"
      style={{ backgroundColor: "#1a2540" }}
    >
      {/* 종목 선택 */}
      <div className="mb-6">
        <label
          className="block text-sm font-semibold mb-3"
          style={{ color: "#d4af37" }}
        >
          이 종목을
        </label>
        <div className="flex flex-wrap gap-2">
          {STOCK_PRESETS.map((preset, idx) => (
            <button
              key={preset.label}
              type="button"
              onClick={() => setSelectedPresetIndex(idx)}
              className="px-4 py-2 rounded-lg text-sm font-medium transition-all"
              style={
                selectedPresetIndex === idx
                  ? {
                    backgroundColor: "#d4af37",
                    color: "#1a1a2e",
                    fontWeight: 700,
                  }
                  : {
                    backgroundColor: "#0f3460",
                    color: "#ffffff",
                    border: "1px solid #2a2a4e",
                  }
              }
            >
              {preset.label}
            </button>
          ))}
        </div>

        {/* 검색 인풋 + 드롭다운 */}
        {isCustom && (
          <div ref={dropdownRef} className="mt-3 relative">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => handleSearchQueryChange(e.target.value)}
              placeholder="종목명 또는 코드 입력 (예: 삼성, AAPL)"
              className="w-full px-4 py-2 rounded-lg text-sm outline-none"
              style={{
                backgroundColor: "#0f3460",
                color: "#ffffff",
                border: "1px solid #d4af37",
              }}
            />

            {/* 검색 중 표시 */}
            {isSearching && (
              <p className="mt-1 text-xs" style={{ color: "#888" }}>
                검색 중...
              </p>
            )}

            {/* 검색 결과 드롭다운 */}
            {showDropdown && (
              <ul
                style={{
                  position: "absolute",
                  zIndex: 10,
                  top: "100%",
                  left: 0,
                  right: 0,
                  backgroundColor: "#0f3460",
                  border: "1px solid #d4af37",
                  borderRadius: "8px",
                  marginTop: "4px",
                  maxHeight: "240px",
                  overflowY: "auto",
                  listStyle: "none",
                  padding: 0,
                  margin: "4px 0 0 0",
                }}
              >
                {searchResults.slice(0, 8).map((result) => (
                  <li
                    key={result.symbol}
                    onClick={() => handleSelectStock(result)}
                    style={{
                      padding: "10px 16px",
                      cursor: "pointer",
                      color: "#ffffff",
                      fontSize: "14px",
                      borderBottom: "1px solid #1a3a6e",
                    }}
                    onMouseEnter={(e) => {
                      (e.currentTarget as HTMLLIElement).style.backgroundColor = "#1a3a6e";
                    }}
                    onMouseLeave={(e) => {
                      (e.currentTarget as HTMLLIElement).style.backgroundColor = "transparent";
                    }}
                  >
                    {result.name} ({result.symbol}, {result.exchange})
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>

      {/* 일일 투자금 */}
      <div className="mb-6">
        <label
          htmlFor="dailyAmount"
          className="block text-sm font-semibold mb-2"
          style={{ color: "#d4af37" }}
        >
          하루 이만큼만 투자했다면
        </label>
        <div className="relative">
          <input
            id="dailyAmount"
            type="number"
            value={dailyAmount}
            onChange={(e) => setDailyAmount(Number(e.target.value))}
            min={1000}
            max={1000000}
            step={1000}
            required
            className="w-full px-4 py-2 rounded-lg text-sm outline-none pr-10"
            style={{
              backgroundColor: "#0f3460",
              color: "#ffffff",
              border: "1px solid #2a2a4e",
            }}
          />
          <span
            className="absolute right-3 top-1/2 -translate-y-1/2 text-sm"
            style={{ color: "#888" }}
          >
            원
          </span>
        </div>
        <p className="mt-1 text-xs" style={{ color: "#888" }}>
          1,000원 ~ 1,000,000원
        </p>
      </div>

      {/* 기간 */}
      <div className="mb-6 flex gap-4">
        <div className="flex-1">
          <label
            htmlFor="startDate"
            className="block text-sm font-semibold mb-2"
            style={{ color: "#d4af37" }}
          >
            이때부터
          </label>
          <input
            id="startDate"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            required
            className="w-full px-4 py-2 rounded-lg text-sm outline-none"
            style={{
              backgroundColor: "#0f3460",
              color: "#ffffff",
              border: "1px solid #2a2a4e",
              colorScheme: "dark",
            }}
          />
        </div>
        <div className="flex-1">
          <label
            htmlFor="endDate"
            className="block text-sm font-semibold mb-2"
            style={{ color: "#d4af37" }}
          >
            이때까지..
          </label>
          <input
            id="endDate"
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            required
            className="w-full px-4 py-2 rounded-lg text-sm outline-none"
            style={{
              backgroundColor: "#0f3460",
              color: "#ffffff",
              border: "1px solid #2a2a4e",
              colorScheme: "dark",
            }}
          />
        </div>
      </div>
      {formError && (
        <p role="alert" aria-live="polite" className="text-rise text-sm mt-1">
          {formError}
        </p>
      )}

      {/* 제출 버튼 */}
      <button
        type="submit"
        disabled={isLoading}
        className="w-full py-3 rounded-xl font-bold text-base transition-all"
        style={{
          backgroundColor: isLoading ? "#a08a20" : "#d4af37",
          color: "#1a1a2e",
          cursor: isLoading ? "not-allowed" : "pointer",
        }}
      >
        {isLoading ? "후회 중..." : "후회 시작"}
      </button>
    </form>
  );
}
