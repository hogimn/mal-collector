import {useMemo} from "react";
import {isMobile} from "react-device-detect";

const POLL_OPTIONS = [1, 2, 3, 4, 5];
const COLOR_PALETTE = [
    "rgba(163, 48, 80, 1)",
    "rgba(139, 63, 124, 1)",
    "rgba(114, 83, 166, 1)",
    "rgba(88, 120, 198, 1)",
    "rgba(74, 144, 226, 1)",
];

const BASE_DATASET_CONFIG = {
    tension: 0.3,
    borderRadius: 8,
    borderWidth: 1.5,
    pointRadius: 3,
    borderDash: [4.5, 4.5],
};

const ZOOM_OPTIONS = {
    zoom: {
        wheel: {enabled: true, modifierKey: "ctrl"},
        pinch: {enabled: true},
        mode: "x",
    },
    pan: {
        enabled: !isMobile,
        mode: "x",
    },
};

export const usePollChartData = (episodeDistribution) => {
    const safeDistribution = useMemo(() => {
        return episodeDistribution && typeof episodeDistribution === "object"
            ? episodeDistribution
            : {};
    }, [episodeDistribution]);

    const {episodes, dataPerEpisode, averageScores} = useMemo(() => {
        const sortedEps = Object.keys(safeDistribution)
            .map((ep) => parseInt(ep, 10))
            .filter((ep) => !isNaN(ep))
            .sort((a, b) => a - b);

        const parsedData = [];
        const avgScores = [];

        sortedEps.forEach((episode) => {
            const epData = safeDistribution[episode] || {};
            const avgScore = parseFloat(epData.averageScore) || 0;

            parsedData.push({
                episode,
                totalVotes: epData.votes || 0,
                averageScore: avgScore,
                optionVotes: POLL_OPTIONS.map((optionId) => epData[optionId] || 0),
            });

            avgScores.push(avgScore);
        });

        return {
            episodes: sortedEps,
            dataPerEpisode: parsedData,
            averageScores: avgScores,
        };
    }, [safeDistribution]);

    const chartData = useMemo(() => {
        return {
            labels: episodes.map((ep) => `Ep.${ep}`),
            datasets: [
                {
                    label: "Avg. Score",
                    type: "line",
                    data: averageScores,
                    borderColor: "rgba(183, 221, 247, 0.9)",
                    pointBackgroundColor: "rgba(255, 255, 255, 0)",
                    yAxisID: "y1",
                    ...BASE_DATASET_CONFIG,
                },
                ...POLL_OPTIONS.map((option, index) => ({
                    label: `★${option}`,
                    data: dataPerEpisode.map((data) => data.optionVotes[index]),
                    borderColor: COLOR_PALETTE[index],
                    backgroundColor: COLOR_PALETTE[index].replace("1)", "0.25)"),
                    stack: "Stack 0",
                    ...BASE_DATASET_CONFIG,
                })),
            ],
        };
    }, [episodes, averageScores, dataPerEpisode]);

    return {episodes, dataPerEpisode, chartData, ZOOM_OPTIONS};
};