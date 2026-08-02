import {StyledHeader, StyledLink,} from "./Header.style";

const Header = () => {

    return (
        <StyledHeader>
            <StyledLink to="/">
                <h2>MAL Collector</h2>
                <p>Data is sourced from MyAnimeList</p>
            </StyledLink>
        </StyledHeader>
    );
};

export default Header;