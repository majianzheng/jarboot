import {memo} from "react";
import {useIntl} from "umi";
import {Button, Space} from "antd";

const OneClickButtons: any = memo((props: any) => {
    const intl = useIntl();

    return <Space size={'middle'} style={{margin: "-20px 0 10px 20px"}}>
        <Button type={'primary'}
                loading={props.oneClickLoading}
                onClick={props.oneClickRestart}>
            {intl.formatMessage({id: 'ONE_KEY_RESTART'})}
        </Button>
        <Button loading={props.oneClickLoading}
                onClick={props.oneClickStart}>
            {intl.formatMessage({id: 'ONE_KEY_START'})}
        </Button>
        <Button loading={props.oneClickLoading}
                onClick={props.oneClickStop}>
            {intl.formatMessage({id: 'ONE_KEY_STOP'})}
        </Button>
    </Space>;
});

export default OneClickButtons;
