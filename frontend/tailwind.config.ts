import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{js,ts,jsx,tsx,mdx}"],
  theme: {
    extend: {
      colors: {
        ink: "#17202a",
        leaf: "#2f7d57",
        coral: "#d65f4a",
        mist: "#f4f7f5",
      },
    },
  },
  plugins: [],
};

export default config;
