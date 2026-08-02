import React from "react";
import CommonCol from "../../../components/base/CommonCol";
import AnimeDescription from "./AnimeDescription";
import AnimeImage from "./AnimeImage";
import AnimePollGraph from "./AnimePollGraph";
import LazyGraphWrapper from "./LazyGraphWrapper";
import {toScoreLabel} from "../../../utils/strUtil";
import {FaStar, FaTrophy, FaUserFriends, FaVoteYea} from "react-icons/fa";
import {MdTrendingUp} from "react-icons/md";
import {AnimeImageWrapper, AnimeSubWrapper, AnimeWrapper, ImageWrapper, OverlayBox,} from "./styles/AnimeCard.style";

const AnimeCard = ({anime, onImageClick}) => {
    return (
        <AnimeWrapper sm={24} md={12} lg={12} xl={8} xxl={8}>
            <AnimeSubWrapper>
                <AnimeImageWrapper>
                    <ImageWrapper onClick={() => onImageClick(anime.largeImage || anime.image)}>
                        <AnimeImage alt={anime.title} src={anime.image}/>
                        <OverlayBox>
                            <span><FaStar title="Score"/> {toScoreLabel(anime.score)}</span>
                            <span><FaVoteYea
                                title="Votes"/> {anime.scoringCount?.toLocaleString() || "N/A"}</span>
                            <span><FaTrophy title="Rank"/> {anime.rank?.toLocaleString() || "N/A"}</span>
                            <span><FaUserFriends
                                title="Members"/> {anime.members?.toLocaleString() || "N/A"}</span>
                            <span><MdTrendingUp
                                title="Popularity"/> {anime.popularity?.toLocaleString() || "N/A"}</span>
                        </OverlayBox>
                    </ImageWrapper>
                    <AnimeDescription anime={anime}/>
                </AnimeImageWrapper>

                <CommonCol style={{display: "flex", flexDirection: "column", alignItems: "flex-start"}}>
                    <LazyGraphWrapper>
                        <AnimePollGraph episodeDistribution={anime.episodeDistribution}/>
                    </LazyGraphWrapper>
                </CommonCol>
            </AnimeSubWrapper>
        </AnimeWrapper>
    );
};

export default React.memo(AnimeCard);