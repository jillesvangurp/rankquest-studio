// must be in the jsMain/resource folder
const mainCssFile = 'styles.css';

const tailwind = {
    darkMode: 'media',
    plugins: [
        require('@tailwindcss/forms'),
        require('daisyui'),
    ],
    variants: {},
    theme: {
        extend: {
            colors: {
                // hsl(210,90,80)
                'blueBright': {
                  '50': '#f0f6fe',
                  '100': '#dcebfd',
                  '200': '#c1ddfc',
                  '300': '#9eccfa',
                  '400': '#65aaf5',
                  '500': '#4188f0',
                  '600': '#2b6ae5',
                  '700': '#2356d2',
                  '800': '#2346aa',
                  '900': '#213f87',
                  '950': '#192852',
                },
                // hsl(210,40,80)
                'blueMuted': {
                     '50': '#f4f7fa',
                     '100': '#e5ebf4',
                     '200': '#d2deeb',
                     '300': '#b8cce0',
                     '400': '#8eaccc',
                     '500': '#7291bf',
                     '600': '#607bb0',
                     '700': '#546aa1',
                     '800': '#495984',
                     '900': '#3e4a6a',
                     '950': '#293042',
                 },
                // hsl(210,15,80)
                'blueGrayMuted': {
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
                'blueGray': {
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
            }
        },
    },
    content: [
        '*.{js,html,css,md}',
        './kotlin/**/*.{js,html,css,md}'
    ],
    daisyui: {
        themes: false, // false: only light + dark | true: all themes | array: specific themes like this ["light", "dark", "cupcake"]
        darkTheme: "dark", // name of one of the included themes for dark mode
        base: true, // applies background color and foreground color for root element by default
        styled: true, // include daisyUI colors and design decisions for all components
        utils: true, // adds responsive and modifier utility classes
        prefix: "", // prefix for daisyUI classnames (components, modifiers and responsive class names. Not colors)
        logs: true, // Shows info about daisyUI version and used config in the console when building your CSS
        themeRoot: ":root", // The element that receives theme color CSS variables
//        themes: [
//            {
//                light: {
//                  ...require("daisyui/src/theming/themes")["light"],
//                  primary: "#2356d2",
//                  secondary: "#6b748a",
//                },
//            }
//        ]
    },
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