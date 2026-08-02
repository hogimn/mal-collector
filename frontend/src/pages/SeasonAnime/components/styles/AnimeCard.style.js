import styled from "styled-components";
import CommonCol from "../../../../components/base/CommonCol";

export const AnimeWrapper = styled(CommonCol)`
    display: flex;
    flex-direction: column;
    position: relative;
  
    .ant-col {
        max-width: 100%;
    }
  
    .ant-card-body {
        display: none;
    }
  
    @media (max-width: 768px) {
        .ant-card-cover {
            width: 160px;
        }
    }
`;

export const AnimeSubWrapper = styled.article`
    background-color: rgba(0, 0, 0, 0.25);
    border: rgba(131, 125, 125, 0.51) 1px solid;
    border-radius: 10px;
    margin: 10px;
  
    section + section {
        margin-top: 10px;
    }
`;

export const AnimeImageWrapper = styled.section`
    display: flex;
    margin-bottom: 8px;
`;

export const OverlayBox = styled.div`
    position: absolute;
    bottom: 10px;
    left: 10px;
    background-color: rgba(0, 0, 0, 0.7);
    color: white;
    padding: 5px 10px;
    border-radius: 10px;
    font-size: 0.7rem;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
  
    svg {
        margin-right: 5px;
    }
`;

export const ImageWrapper = styled.div`
    display: inline-block;
    cursor: pointer;
    border-top-left-radius: 9px;
    overflow: hidden;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    position: relative;
`;