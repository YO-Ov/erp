import { statusMeta, type StatusMap } from '../domain/status'

// 상태 코드 → 한글 라벨 배지.
// 색은 CSS(.badge.tone-*)가 테마별로 정한다 — 다크/라이트 각각 정의돼 있어
// 라이트 배경에서도 텍스트 대비가 확보된다.
// map 과 status 를 같은 K 로 묶어서, 엉뚱한 상태맵에 엉뚱한 상태를 넘기면 컴파일에서 걸린다.
export default function StatusBadge<K extends string>({
  map,
  status,
}: {
  map: StatusMap<K>
  status: K
}) {
  const meta = statusMeta(map, status)
  return <span className={`badge tone-${meta.tone}`}>{meta.label}</span>
}
