import React from "react";
import CommonModal from "../../../../components/base/CommonModal";
import ModalButton from "../ModalButton";
import {
    EpisodeItem,
    EpisodeList,
    ProgressBarBackground,
    ProgressBarFill,
    ScrollableContent,
    StyledTotalVotes,
    VoteItem,
    VoteList,
} from "../styles/AnimePollGraph.style";

const PollModal = ({
                       isOpen,
                       onClose,
                       showAllEpisodes,
                       modalData,
                       episodes,
                       currentIdx,
                       onSelectEpisode,
                       onShowAllEpisodes,
                       onNavigate,
                   }) => {
    if (!isOpen || !modalData) return null;

    return (
        <CommonModal
            title={showAllEpisodes ? "All Episodes" : `Episode ${modalData.episode}`}
            open={isOpen}
            onCancel={onClose}
            footer={null}
            centered
        >
            <ScrollableContent>
                {showAllEpisodes ? (
                    <EpisodeList>
                        {episodes.map((ep) => (
                            <EpisodeItem key={ep} onClick={() => onSelectEpisode(ep)}>
                                Episode {ep}
                            </EpisodeItem>
                        ))}
                    </EpisodeList>
                ) : (
                    <>
                        <StyledTotalVotes>
                            Average Score: {modalData.averageScore.toFixed(2)}
                        </StyledTotalVotes>
                        <StyledTotalVotes marginBottom={"1rem"}>
                            Total Votes: {modalData.totalVotes}
                        </StyledTotalVotes>
                        <VoteList>
                            {[...modalData.votesBreakdown]
                                .reverse()
                                .map(({option, votes, percentage}) => (
                                    <VoteItem key={option}>
                                        ★{option}: {votes} votes ({percentage}%)
                                        <ProgressBarBackground>
                                            <ProgressBarFill width={percentage}/>
                                        </ProgressBarBackground>
                                    </VoteItem>
                                ))}
                        </VoteList>
                        <div
                            style={{
                                display: "flex",
                                justifyContent: "space-between",
                                marginTop: "1rem",
                                gap: "0.5rem",
                            }}
                        >
                            <ModalButton
                                onClick={() => onNavigate(-1)}
                                disabled={currentIdx <= 0}
                            >
                                Prev
                            </ModalButton>
                            <ModalButton onClick={onShowAllEpisodes}>
                                All Episodes
                            </ModalButton>
                            <ModalButton
                                onClick={() => onNavigate(1)}
                                disabled={currentIdx === -1 || currentIdx >= episodes.length - 1}
                            >
                                Next
                            </ModalButton>
                        </div>
                    </>
                )}
            </ScrollableContent>
        </CommonModal>
    );
};

export default PollModal;