const antlr4 = require("antlr4/index");
const CommonToken = require("antlr4/Token").CommonToken;
const wdlLexer = require("./WdlLexer");
const uunescape = require("unescape-unicode");


function WDLBaseLexer(input) {
    antlr4.Lexer.call(this, input);
    this.curlyStack = [];
    this._previousTokenType = null;
    return this;
}

WDLBaseLexer.prototype = Object.create(antlr4.Lexer.prototype);
WDLBaseLexer.prototype.constructor = antlr4.Lexer;

WDLBaseLexer.prototype.nextToken = function () {
    let token = antlr4.Lexer.prototype.nextToken.call(this);
    let currentToken = token.type;
    if (this._mode === wdlLexer.WdlLexer.SquoteInterpolatedString && token.type === wdlLexer.WdlLexer.SQuoteUnicodeEscape) {
        let text = this.unescape(token.text);
        token = new CommonToken(type = wdlLexer.WdlLexer.SQuoteStringPart);
        token.text = text;
    } else if (this._mode === wdlLexer.WdlLexer.DquoteInterpolatedString && token.type === wdlLexer.WdlLexer.DQuoteUnicodeEscape) {
        let text = this.unescape(token.text);
        token = new CommonToken(type = wdlLexer.WdlLexer.DQuoteStringPart);
        token.text = text;
    } else if (this._mode === wdlLexer.WdlLexer.Command && token.type === wdlLexer.WdlLexer.CommandUnicodeEscape) {
        let text = this.unescape(token.text);
        token = new CommonToken(type = wdlLexer.WdlLexer.CommandStringPart);
        token.text = text;
    } else if (this._mode === wdlLexer.WdlLexer.HereDocCommand && token.type === wdlLexer.WdlLexer.HereDocUnicodeEscape) {
        let text = this.unescape(token.text);
        token = new CommonToken(type = wdlLexer.WdlLexer.HereDocStringPart);
        token.text = text;
    }

    if (this._channel === antlr4.Lexer.DEFAULT_TOKEN_CHANNEL) {
        this._previousTokenType = currentToken;
    }

    return token;
};

WDLBaseLexer.prototype.unescape = function (text) {
    return uunescape(text);
};

/**
 * @return {boolean}
 */
WDLBaseLexer.prototype.IsCommand = function () {
    return this._previousTokenType === wdlLexer.WdlLexer.COMMAND;
};

WDLBaseLexer.prototype.PopModeOnCurlBracketClose = function () {
    if (this.curlyStack.length > 0) {
        if (this.curlyStack.pop()) {
            this.popMode();
        }
    }
};

WDLBaseLexer.prototype.PopCurlBrackOnClose = function () {
    this.curlyStack.pop();
};

WDLBaseLexer.prototype.PushCommandAndBrackEnter = function () {
    this.pushMode(wdlLexer.WdlLexer.Command);
    this.curlyStack.push(true);
};

WDLBaseLexer.prototype.PushCurlBrackOnEnter = function (shouldPop) {
    this.curlyStack.push(shouldPop === 1);
};

exports.WDLBaseLexer = WDLBaseLexer;
