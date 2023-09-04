// must be in the jsMain/resource folder
const mainCssFile = 'styles.css';

const tailwind = {
    darkMode: 'media',
    plugins: [
        require('@tailwindcss/forms')
    ],
    variants: {},
    theme: {
        extend: {
            colors: {
                // hsl(210,60,80)
                'button': {
                  '50': '#f2f6fc',
                  '100': '#e1ebf8',
                  '200': '#cbdcf2',
                  '300': '#adcbeb',
                  '400': '#7ca9de',
                  '500': '#5e8bd3',
                  '600': '#4a73c6',
                  '700': '#405fb5',
                  '800': '#394f94',
                  '900': '#324576',
                  '950': '#222b49',
                },
                // hsl(210,30,80)
                'buttonSecondary': {
                       '50': '#f5f7f9',
                       '100': '#e7ecf2',
                       '200': '#d5dee8',
                       '300': '#bdccdb',
                       '400': '#95acc5',
                       '500': '#7c94b5',
                       '600': '#6a7fa6',
                       '700': '#5e6f97',
                       '800': '#505c7d',
                       '900': '#444d64',
                       '950': '#2c313f',
                },
                // hsl(210,15,80)
                'buttonNav': {
                    '50': '#f6f7f8',
                    '100': '#eaecef',
                    '200': '#d9dee4',
                    '300': '#c4ccd4',
                    '400': '#a0adba',
                    '500': '#8996a8',
                    '600': '#788398',
                    '700': '#6b748a',
                    '800': '#5b6172',
                    '900': '#4b515d',
                    '950': '#31333a',
                },
                // hsl(210,15,30)
                'buttonNavAct': {
                    '50': '#f4f6f7',
                    '100': '#e2e7eb',
                    '200': '#c9d2d8',
                    '300': '#a3b2bd',
                    '400': '#758a9b',
                    '500': '#5a6e80',
                    '600': '#4d5d6d',
                    '700': '#414c58',
                    '800': '#3c444e',
                    '900': '#353b44',
                    '950': '#21252b',
                },



//                'primary': {
//                    DEFAULT: '#002EA7',
//                    '50': '#D2DFFF',
//                    '100': '#B1C7FF',
//                    '200': '#6F97FF',
//                    '300': '#2D67FF',
//                    '400': '#0040E9',
//                    '500': '#002EA7',
//                    '600': '#00237E',
//                    '700': '#001855',
//                    '800': '#000C2D',
//                    '900': '#000104'
//                },
//                'secondary': {
//                    DEFAULT: '#DA291C',
//                    '50': '#FBE2E0',
//                    '100': '#F8CDCA',
//                    '200': '#F2A29D',
//                    '300': '#ED786F',
//                    '400': '#E74D42',
//                    '500': '#DA291C',
//                    '600': '#AD2116',
//                    '700': '#801810',
//                    '800': '#520F0B',
//                    '900': '#250705'
//                },
//                'tertiary': {
//                    DEFAULT: '#A7C452',
//                    '50': '#FEFEFD',
//                    '100': '#F4F8EA',
//                    '200': '#E1EBC4',
//                    '300': '#CEDE9E',
//                    '400': '#BAD178',
//                    '500': '#A7C452',
//                    '600': '#8DA93A',
//                    '700': '#6D832D',
//                    '800': '#4E5D20',
//                    '900': '#2E3713'
//                },
                'success': {
                    DEFAULT: '#58C322',
                    '50': '#DFF7D3',
                    '100': '#D0F4BD',
                    '200': '#B0EC92',
                    '300': '#91E467',
                    '400': '#71DD3B',
                    '500': '#58C322',
                    '600': '#44981A',
                    '700': '#316C13',
                    '800': '#1D410B',
                    '900': '#0A1504'
                },
                'warning': {
                    DEFAULT: '#FFAB1A',
                    '50': '#FFFFFF',
                    '100': '#FFF6E6',
                    '200': '#FFE3B3',
                    '300': '#FFD080',
                    '400': '#FFBE4D',
                    '500': '#FFAB1A',
                    '600': '#E69200',
                    '700': '#B37100',
                    '800': '#805100',
                    '900': '#4D3100'
                },
                'error': {
                    DEFAULT: '#D41111',
                    '50': '#FBCFCF',
                    '100': '#F9B8B8',
                    '200': '#F58989',
                    '300': '#F25959',
                    '400': '#EE2A2A',
                    '500': '#D41111',
                    '600': '#A50D0D',
                    '700': '#760909',
                    '800': '#460606',
                    '900': '#170202'
                },
            }
        },
    },
    content: [
        '*.{js,html,css}',
        './kotlin/**/*.{js,html,css}'
    ]
};


// webpack tailwind css settings
((config) => {
    ((config) => {
        let entry = '../../../processedResources/js/main/' + mainCssFile;
        config.entry.main.push(entry);
        config.module.rules.push({
            test: /\.css$/,
            use: [
                {loader: 'style-loader'},
                {loader: 'css-loader'},
                {
                    loader: 'postcss-loader',
                    options: {
                        postcssOptions: {
                            plugins: [
                                require("tailwindcss")({config: tailwind}),
                                require("autoprefixer"),
                                require("cssnano")
                            ]
                        }
                    }
                }
            ]
        });
    })(config);
})(config);