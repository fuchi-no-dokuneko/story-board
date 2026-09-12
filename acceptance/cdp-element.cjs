class CdpElement {
  constructor(driver, selector, index = 0) { Object.assign(this, { driver, selector, index }); }
  expression() { return `document.querySelectorAll(${JSON.stringify(this.selector)})[${this.index}]`; }
  async getText() { return this.driver.evaluate(`${this.expression()}.innerText`); }
  async getAttribute(name) {
    return this.driver.evaluate(`((element) => ${name === 'value' ? 'element.value' : `element.getAttribute(${JSON.stringify(name)})`})(${this.expression()})`);
  }
  async isDisplayed() {
    return this.driver.evaluate(`((e) => !!e && !!(e.offsetWidth || e.offsetHeight || e.getClientRects().length) && getComputedStyle(e).visibility !== 'hidden')(${this.expression()})`);
  }
  async isEnabled() { return this.driver.evaluate(`!${this.expression()}.disabled`); }
  async click() {
    const point = await this.driver.evaluate(`((e) => { e.scrollIntoView({block:'center'}); const r=e.getBoundingClientRect(); return {x:r.x+r.width/2,y:r.y+r.height/2}; })(${this.expression()})`);
    await this.driver.sendDevToolsCommand('Input.dispatchMouseEvent', { type: 'mousePressed', button: 'left', clickCount: 1, ...point });
    await this.driver.sendDevToolsCommand('Input.dispatchMouseEvent', { type: 'mouseReleased', button: 'left', clickCount: 1, ...point });
  }
}
const By = { css: value => value };
const until = {
  elementLocated: selector => async driver => (await driver.findElements(selector))[0],
  elementIsVisible: element => () => element.isDisplayed(),
  elementIsEnabled: element => () => element.isEnabled(),
};
class Select {
  constructor(element) { this.element = element; }
  async selectByValue(value) { return this.select('value', value); }
  async selectByVisibleText(value) { return this.select('textContent', value); }
  async select(property, value) {
    await this.element.driver.executeScript(`const e=arguments[0]; const option=[...e.options].find(o=>o[arguments[1]]===arguments[2]); if(!option) throw Error('Option absent'); e.value=option.value; e.dispatchEvent(new Event('change',{bubbles:true}));`, this.element, property, value);
  }
}
module.exports = { CdpElement, By, until, Select };
