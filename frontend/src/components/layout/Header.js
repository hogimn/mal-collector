import Logo from "./Logo";
import {getImagePath} from "../../utils/pathUtil";
import {StyledHeader, StyledLink,} from "./Header.style";

const Header = () => {

    return (
        <StyledHeader>
            <StyledLink to="/">
                <Logo
                    src={getImagePath("logo.png")}
                    alt={"MAL Collector Logo"}
                    width={"70px"}
                />
            </StyledLink>
            <StyledLink to="/">
                <h2>MAL Collector</h2>
                <p>Data is sourced from MyAnimeList</p>
            </StyledLink>
        </StyledHeader>
    );
};

export default Header;