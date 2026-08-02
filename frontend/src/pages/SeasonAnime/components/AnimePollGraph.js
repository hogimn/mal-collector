import React, {useCallback, useEffect, useRef, useState} from "react";
import {Bar} from "react-chartjs-2";
import {usePollChartData} from "./hooks/usePollChartData";
import {useChartOptions} from "./hooks/useChartOptions";
import PollModal from "./modals/PollModal";

const POLL_OPTIONS = [1, 2, 3, 4, 5];

const AnimePollGraph = ({episodeDistribution}) => {
    const chartRef = useRef(null);

    const [modalData, setModalData] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [showAllEpisodes, setShowAllEpisodes] = useState(false);

    const {episodes, dataPerEpisode, chartData, ZOOM_OPTIONS} = usePollChartData(episodeDistribution);

    const openEpisodeModal = useCallback(
        (episode) => {
            const data = dataPerEpisode.find((d) => d.episode === episode);
            if (!data) return;

            setModalData({
                episode: data.episode,
                totalVotes: data.totalVotes,
                averageScore: data.averageScore,
                votesBreakdown: POLL_OPTIONS.map((option, i) => {
                    const votes = data.optionVotes[i];
                    const percentage =
                        data.totalVotes > 0
                            ? ((votes / data.totalVotes) * 100).toFixed(1)
                            : "0.0";
                    return {option, votes, percentage};
                }),
            });
            setIsModalOpen(true);
            setShowAllEpisodes(false);
        },
        [dataPerEpisode]
    );

    const options = useChartOptions({
        chartData,
        dataPerEpisode,
        episodes,
        ZOOM_OPTIONS,
        openEpisodeModal,
    });

    useEffect(() => {
        const handleTouchEnd = (event) => {
            const canvas = chartRef.current?.canvas;
            if (canvas && event.target !== canvas) {
                canvas.dispatchEvent(new Event("mouseout"));
            }
        };

        document.addEventListener("touchend", handleTouchEnd);
        return () => {
            document.removeEventListener("touchend", handleTouchEnd);
        };
    }, []);

    const currentIdx = modalData ? episodes.indexOf(modalData.episode) : -1;

    const handleNavigate = (direction) => {
        const targetEpisode = episodes[currentIdx + direction];
        if (targetEpisode !== undefined) {
            openEpisodeModal(targetEpisode);
        }
    };

    return (
        <>
            <Bar ref={chartRef} options={options} data={chartData}/>

            <PollModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                showAllEpisodes={showAllEpisodes}
                modalData={modalData}
                episodes={episodes}
                currentIdx={currentIdx}
                onSelectEpisode={(ep) => openEpisodeModal(ep)}
                onShowAllEpisodes={() => setShowAllEpisodes(true)}
                onNavigate={handleNavigate}
            />
        </>
    );
};

export default AnimePollGraph;