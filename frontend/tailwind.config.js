/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["Work Sans", "sans-serif"],
        mono: ["IBM Plex Mono", "monospace"],
      },
      colors: {
        primary: {
          DEFAULT: "#605eff",
          light: "#7b7aff",
          dim: "rgba(96,94,255,.1)",
          hover: "#4f4de6",
        },
        success: { DEFAULT: "#2f855a", bg: "#f0fff4", dot: "#48bb78" },
        warning: { DEFAULT: "#dd6b20", bg: "#fff8e1", dot: "#f6ad55" },
        danger: { DEFAULT: "#dc2626", bg: "#fef2f2", dot: "#fc8181" },
        surface: {
          50: "#f8f9fc",
          100: "#f1f3f8",
          200: "#e8e3ed",
          300: "#d4cfe0",
          400: "#b0aac0",
          500: "#7a798c",
          700: "#3d3b52",
          900: "#010113",
        },
        ink: "#010113",
        ink2: "#636778",
        ink3: "#7a798c",
        sidebar: "#f0f0ff",
        page: "#f5f5fa",
      },
      boxShadow: {
        card: "0 1px 3px rgba(0,0,0,.06), 0 1px 2px rgba(0,0,0,.04)",
        "card-md": "0 4px 12px rgba(0,0,0,.08)",
      },
    },
  },
  plugins: [],
};
