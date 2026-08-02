import styled from "styled-components";
import {Link} from "react-router-dom";

export const StyledLink = styled(Link)`
    text-decoration: none;
    color: inherit;
  
    h2 {
        margin-top: 9px;
        margin-bottom: -7px;
    }
  
    p {
        font-size: 10px;
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