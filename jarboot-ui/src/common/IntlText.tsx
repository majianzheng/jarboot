import {useIntl} from "umi";

const IntlText = (props: {id: string}) => {
    const intl = useIntl();
    return (<>{intl.formatMessage(props)}</>);
};

export default IntlText;
