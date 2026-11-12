import '@testing-library/jest-dom/vitest'

// Provide ResizeObserver polyfill for Recharts inside jsdom tests
if (typeof window !== 'undefined' && !window.ResizeObserver) {
  window.ResizeObserver = class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
}

