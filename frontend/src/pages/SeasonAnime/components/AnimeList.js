import {useState} from "react";
import CommonRow from "../../../components/base/CommonRow";
import CommonAlert from "../../../components/base/CommonAlert";
import CommonPagination from "../../../components/base/CommonPagination";
import CommonModal from "../../../components/base/CommonModal";
import AnimeCard from "./AnimeCard";
import {useAnimeData} from "./hooks/useAnimeData";
import {StyledSpin} from "./styles/AnimeList.style";

const AnimeList = (props) => {
    const {page, setPage, pageSize} = props;

    const {currentPageAnimes, totalCount, loading, error} = useAnimeData(props);

    const [selectedImage, setSelectedImage] = useState("");

    const handleImageClick = (src) => {
        setSelectedImage(src);
    };

    const closeModal = () => {
        setSelectedImage("");
    };

    if (loading) return <StyledSpin tip="Loading..."/>;
    if (error) return <CommonAlert message={error} type="error"/>;
    if (!totalCount) return <p>No anime found</p>;

    return (
        <>
            <CommonRow>
                {currentPageAnimes.map((anime) => (
                    <AnimeCard
                        key={`anime-card-${anime.id}`}
                        anime={anime}
                        onImageClick={handleImageClick}
                    />
                ))}
            </CommonRow>

            <CommonPagination
                current={page}
                pageSize={pageSize}
                total={totalCount}
                onChange={setPage}
                style={{marginTop: "16px", textAlign: "center"}}
                showSizeChanger={false}
            />

            <CommonModal
                open={!!selectedImage}
                onCancel={closeModal}
                footer={null}
                centered
            >
                {selectedImage && (
                    <img
                        src={selectedImage}
                        alt="Anime preview"
                        style={{
                            width: "100%",
                            height: "auto",
                            maxHeight: "80vh",
                            objectFit: "contain",
                        }}
                    />
                )}
            </CommonModal>
        </>
    );
};

export default AnimeList;