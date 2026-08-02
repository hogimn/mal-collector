import styled from "styled-components";
import CommonTabs from "../../../../components/base/CommonTabs";

export const StyledSeasonTab = styled.div`
    p {
        margin-left: 10px;
    }
`;

export const SelectWrapper = styled.div`
    margin-top: 16px;
    margin-bottom: 16px;
    text-align: left;
    .ant-select {
        min-width: fit-content;
        margin-top: 5px;
        margin-left: 10px;
    }
`;

export const CustomTabs = styled(CommonTabs)`
    .ant-tabs-nav-wrap {
        margin: 0 10px;
    }
    .ant-tabs-tab + .ant-tabs-tab {
        margin: 0 0 0 0;
    }
    .ant-tabs-tab {
        padding: 12px 24px;
    }
`;
