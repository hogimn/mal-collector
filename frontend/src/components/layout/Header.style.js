import styled from "styled-components";
import { Link } from "react-router-dom";

export const StyledLink = styled(Link)`
    text-decoration: none;
    color: inherit;
    display: flex;
    align-items: flex-end;
    padding: 12px 0;
    gap: 8px;
  
    h2 {
        padding-left: 10px;
        margin: 0 0 0 0;
        font-size: 2rem;
        font-family: Avenir, sans-serif;
        font-weight: bold;
        letter-spacing: -2.5px;
    }
  
    p {
        margin: 0;
        font-size: 10px;
        padding-bottom: 2px;
        letter-spacing: -0.5px;
    }
`;

export const StyledHeader = styled.header`
    display: flex;
    align-items: center;
    font-size: 1.15em;
  
    h1 {
        margin-left: 10px;
    }
  
    ${StyledLink} + ${StyledLink} {
        margin-left: 5px;
    }
  
    .ant-btn {
        margin-left: auto;
        margin-right: 10px;
        background-color: rgba(36, 46, 66, 0.7);
        border: 1px solid rgba(25, 26, 46, 0.7);
        padding-left: 7px;
        padding-right: 7px;
    }
  
    .ant-btn:hover {
        background-color: rgba(96, 119, 160, 0.7) !important;
    }
  
    .ant-alert {
        margin-right: 10px;
    }
`;