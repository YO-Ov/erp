// 다크/라이트 테마 — CSS 변수를 :root[data-theme] 로 오버라이드한다.
// 선택은 localStorage 에 저장하고, 없으면 시스템 설정(prefers-color-scheme)을 따른다.

export type Theme = 'dark' | 'light'

const KEY = 'erp_theme'

export function getInitialTheme(): Theme {
  const saved = localStorage.getItem(KEY)
  if (saved === 'dark' || saved === 'light') return saved
  // 저장값이 없으면 OS 설정을 존중한다.
  return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
}

export function applyTheme(theme: Theme): void {
  document.documentElement.setAttribute('data-theme', theme)
  localStorage.setItem(KEY, theme)
}
