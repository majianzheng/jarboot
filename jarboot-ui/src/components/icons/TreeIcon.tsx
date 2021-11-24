import Icon from "@ant-design/icons";

const TreeSvg = (props: any) => (
    <svg className="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg"
         width="1em" height="1em" {...props}>
        <path
            d="M384 128c11.39 0 19.014 5.924 22.545 9.455C410.076 140.986 416 148.61 416 160v64c0 11.39-5.924 19.014-9.455 22.545C403.014 250.076 395.39 256 384 256H192c-11.39 0-19.014-5.924-22.545-9.455C165.924 243.014 160 235.39 160 224v-64c0-11.39 5.924-19.014 9.455-22.545C172.986 133.924 180.61 128 192 128h192m0-64H192c-52.8 0-96 43.2-96 96v64c0 52.8 43.2 96 96 96h192c52.8 0 96-43.2 96-96v-64c0-52.8-43.2-96-96-96zM832 768c11.39 0 19.014 5.924 22.545 9.455C858.076 780.986 864 788.61 864 800v64c0 11.39-5.924 19.014-9.455 22.545C851.014 890.076 843.39 896 832 896H640c-11.39 0-19.014-5.924-22.545-9.455C613.924 883.014 608 875.39 608 864v-64c0-11.39 5.924-19.014 9.455-22.545C620.986 773.924 628.61 768 640 768h192m0-64H640c-52.8 0-96 43.2-96 96v64c0 52.8 43.2 96 96 96h192c52.8 0 96-43.2 96-96v-64c0-52.8-43.2-96-96-96zM832 448c11.39 0 19.014 5.924 22.545 9.455C858.076 460.986 864 468.61 864 480v64c0 11.39-5.924 19.014-9.455 22.545C851.014 570.076 843.39 576 832 576H640c-11.39 0-19.014-5.924-22.545-9.455C613.924 563.014 608 555.39 608 544v-64c0-11.39 5.924-19.014 9.455-22.545C620.986 453.924 628.61 448 640 448h192m0-64H640c-52.8 0-96 43.2-96 96v64c0 52.8 43.2 96 96 96h192c52.8 0 96-43.2 96-96v-64c0-52.8-43.2-96-96-96z"
            p-id="29767"></path>
        <path d="M575.094 800H288c-17.6 0-32 14.4-32 32s14.4 32 32 32h287.094v-64z" p-id="29768"></path>
        <path d="M320 288h-64v544c0 17.6 14.4 32 32 32s32-14.4 32-32V288z" p-id="29769"></path>
        <path d="M576 481H288v64h288v-64z" p-id="29770"></path>
    </svg>);

const TreeIcon = (props: any) => <Icon component={TreeSvg} {...props} />;

export default TreeIcon;