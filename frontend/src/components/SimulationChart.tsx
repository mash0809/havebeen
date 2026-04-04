"use client";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { SimulationResult, ChartPoint } from "@/types/simulation";

interface SimulationChartProps {
  result: SimulationResult;
}

function formatYAxis(value: number): string {
  if (value >= 100_000_000) return `${(value / 100_000_000).toFixed(1)}억`;
  if (value >= 10_000) return `${(value / 10_000).toFixed(0)}만`;
  return String(value);
}

function formatTooltipValue(value: number): string {
  return `${value.toLocaleString()}원`;
}

// x축 레이블: 데이터 포인트가 많으면 일부만 표시
function tickFormatter(value: string, index: number, data: ChartPoint[]): string {
  // 데이터 총 개수에 따라 표시 간격 조정
  const step = Math.max(1, Math.floor(data.length / 6));
  if (index % step !== 0) return "";
  return value.slice(0, 7); // "YYYY-MM" 형식
}

export default function SimulationChart({ result }: SimulationChartProps) {
  const isPositive = result.returnRate >= 0;
  // globals.css @theme 변수를 참조해 테마 일관성 유지
  const lineColor = isPositive ? "var(--color-rise)" : "var(--color-fall)";

  const { chartData } = result;

  return (
    <div
      className="w-full max-w-lg mx-auto p-6 rounded-2xl"
      style={{ backgroundColor: "#16213e" }}
    >
      <p className="text-sm font-semibold mb-4 text-gold">
        투자 평가금액 추이
      </p>
      <ResponsiveContainer width="100%" height={240}>
        <LineChart
          data={chartData}
          margin={{ top: 4, right: 8, left: 8, bottom: 4 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#2a2a4e" />
          <XAxis
            dataKey="date"
            tick={{ fill: "#888", fontSize: 11 }}
            tickLine={false}
            axisLine={{ stroke: "#2a2a4e" }}
            tickFormatter={(value, index) =>
              tickFormatter(value, index, chartData)
            }
          />
          <YAxis
            tick={{ fill: "#888", fontSize: 11 }}
            tickLine={false}
            axisLine={{ stroke: "#2a2a4e" }}
            tickFormatter={formatYAxis}
            width={52}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: "#0f3460",
              border: "1px solid #2a2a4e",
              borderRadius: 8,
              color: "#ffffff",
              fontSize: 12,
            }}
            labelStyle={{ color: "#d4af37" }}
            formatter={(value: number) => [formatTooltipValue(value), "평가금액"]}
          />
          <Line
            type="monotone"
            dataKey="value"
            stroke={lineColor}
            strokeWidth={2}
            dot={false}
            activeDot={{ r: 4, fill: lineColor }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
