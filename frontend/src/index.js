import React from "react";
import ReactDOM from "react-dom/client";
import "./index.css";
import App from "./App";
import {ConfigProvider} from "antd";

const darkTheme = {
    token: {
        colorBgBase: "#141414",
        colorTextBase: "#ffffff",
        colorPrimary: "#a7ccf1",
        colorLink: "#4164fd",
        colorBorder: "#444",
        fontFamily: "Avenir, sans-serif",
        fontSize: 12,
    },
};

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(
    <ConfigProvider theme={darkTheme}>
        <App/>
    </ConfigProvider>
);