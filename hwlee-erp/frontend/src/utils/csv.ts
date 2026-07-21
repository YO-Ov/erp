// 목록 데이터를 CSV(엑셀)로 내려받기 위한 공통 유틸.
// 옛 Thymeleaf 화면의 ERP.toCsv / ERP.download 대응 — 외부 라이브러리 없이 순수 구현.

export interface CsvColumn<T> {
  label: string
  value: (row: T) => string | number | null | undefined
}

// 콤마·따옴표·개행이 들어간 값은 따옴표로 감싸고, 내부 따옴표는 "" 로 이스케이프한다.
function escapeCell(v: string | number | null | undefined): string {
  const s = v == null ? '' : String(v)
  return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
}

export function toCsv<T>(rows: T[], columns: CsvColumn<T>[]): string {
  const header = columns.map((c) => escapeCell(c.label)).join(',')
  const body = rows
    .map((row) => columns.map((c) => escapeCell(c.value(row))).join(','))
    .join('\r\n')
  return `${header}\r\n${body}`
}

// UTF-8 BOM(﻿)을 앞에 붙여야 Excel 에서 한글이 깨지지 않는다.
export function downloadCsv(filename: string, csv: string): void {
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

// 파일명 접미용 오늘 날짜 'YYYY-MM-DD'.
export function todayStamp(): string {
  return new Date().toISOString().slice(0, 10)
}
