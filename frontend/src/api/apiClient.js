import axios from "axios";

const GATEWAY_URL = process.env.REACT_APP_GATEWAY_URL;
export const BASE_URL = `${GATEWAY_URL}/api`;
console.log(BASE_URL)

const apiClient = axios.create({
    baseURL: BASE_URL,
    timeout: 5000,
});

export default apiClient;