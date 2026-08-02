import Header from "./Header";
import Footer from "./Footer";
import {useLocation, useNavigate} from "react-router-dom";
import {
    BackgroundPage,
    CurrentPagePanel,
    MainWrapper,
    MenuItem,
    MenuWrapper,
    Navigation,
    PageWrapper,
    SubMenu,
} from "./PageTemplate.style";

const PageTemplate = ({children}) => {
    const navigate = useNavigate();
    const location = useLocation();

    const getCurrentPage = () => {
        const path =
            location.pathname === "/" ? "home" : location.pathname.slice(1);
        return path
            .split("-")
            .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
            .join(" ");
    };

    return (
        <BackgroundPage>
            <PageWrapper>
                <Header/>
                <Navigation>
                    <MenuWrapper>
                        <MenuItem>
                            Anime
                            <SubMenu>
                                <li onClick={() => navigate("/")}>Season Anime</li>
                                <li onClick={() => navigate("/season-archive")}>
                                    Season Archive
                                </li>
                            </SubMenu>
                        </MenuItem>
                    </MenuWrapper>
                </Navigation>
                <CurrentPagePanel>{getCurrentPage()}</CurrentPagePanel>
                <MainWrapper>{children}</MainWrapper>
                <Footer/>
            </PageWrapper>
        </BackgroundPage>
    );
};

export default PageTemplate;