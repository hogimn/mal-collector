import styled from "styled-components";

const Logo = styled.img`
    width: ${({width}) => width || "100px"};
    height: auto;
`;

export default Logo;