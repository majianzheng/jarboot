import {memo} from "react";
import {useIntl} from "umi";

const IntlFormat: any = memo((props: any) => {
    const intl = useIntl();

    if (props.args) {
        return <>{intl.formatMessage({id: props.id}, {...props.args})}</>;
    }
    return <>{intl.formatMessage({id: props.id})}</>;
});

const formatMsg = (id: string, args?: any) => {

    return <IntlFormat id={id} args={args}/>;
};

export {IntlFormat, formatMsg};
