const cleanSamples = () => {
    const samples = document.querySelectorAll('code.runnablesample');
    samples.forEach(sample => {
        const hasSample = sample.textContent.match(/^\s*\/\/\s*sampleStart\s*$/im) &&
            sample.textContent.match(/^\s*\/\/\s*sampleEnd\s*$/im);

        if (hasSample) {
            let inSample = false
            let newContent = ''
            sample.textContent.split('\n').forEach(line => {
                if (line.match(/^\s*\/\/\s*sampleStart\s*$/im)) {
                    inSample = true;
                } else if (line.match(/^\s*\/\/\s*sampleEnd\s*$/im)) {
                    inSample = false;
                    newContent += /^(\s*)\//.exec(line)[1] + '// ...\n';
                } else if (inSample) {
                    newContent += line + '\n';
                }
            });

            if (newContent.endsWith('// ...\n')) {
                newContent = newContent.slice(0, -7);
            }

            sample.textContent = newContent.trim();
        }
    });
};