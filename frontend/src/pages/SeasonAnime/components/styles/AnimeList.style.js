import styled from "styled-components";
import CommonSpin from "../../../../components/base/CommonSpin";

export const StyledSpin = styled(CommonSpin)`
    display: flex;
    justify-content: center;
    align-items: center;
    height: ${(props) => props?.height || "100vh"};
`;
