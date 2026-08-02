import {useCallback, useEffect, useRef, useState} from "react";

const useLazyLoad = () => {
    const [isVisible, setIsVisible] = useState(false);

    const elementRef = useRef(null);

    const setRef = useCallback((node) => {
        if (node) {
            elementRef.current = node;
        }
    }, []);

    useEffect(() => {
        const target = elementRef.current;
        if (!target) return;

        const observer = new IntersectionObserver(([entry]) => {
            if (entry.isIntersecting) {
                setIsVisible(true);
                observer.disconnect();
            }
        });

        observer.observe(target);

        return () => {
            observer.disconnect();
        };
    }, [setRef]);

    return [setRef, isVisible];
};

export default useLazyLoad;