// Official nerd javascript client

function Nerd() {
    this.host= "";
    this.port= "";
    this.prefix= "";

    this.getUrl = function (postfix) {
        var url = "http://" + this.host;

        if (url.endsWith("/"))
            url = url.substring(0, url.length() - 1);

        if ((!this.port) || (this.port.length === 0))
            url += this.port + postfix;
        else
            url += ":" + this.port + this.prefix + postfix;


        return url;

    };

    this.fetchConcept = function (identifier, lang, successFunction) {
        $.ajax({
            type: 'GET',
            url: this.getUrl('/service/kb/concept/') + identifier + '?lang=' + lang,
            success: successFunction,
            dataType: 'json'
        });
    };
};