function renderAccountHeroCard(d) {
  const bg = d.colorHex || 'var(--accent)';
  const cur = d.currency;
  const perspective = d.perspective || 'Accounting';
  const cardId = d.cardId;

  let chips = '';
  if (d.showCurrencyBadges && d.distribution) {
    const dist = Object.entries(d.distribution).sort((a,b) => b[1] - a[1]);
    if (dist.length > 1) {
      chips = `<div class="hero-distribution" style="display: flex; gap: 8px; overflow-x: auto; padding-bottom: 4px; margin-top: 20px">`;
      dist.forEach(([ccy, bal]) => {
        chips += `<div class="ccy-chip" style="background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.1); padding: 4px 10px; border-radius: 12px; font-size: 0.75rem; white-space: nowrap; color: #fff; cursor: pointer"
                      onclick="navigateWithFilter('${esc(cardId)}', {currencies:['${esc(ccy)}']})">
          <b style="opacity: 0.8; margin-right: 4px">${ccy}</b> ${fmt(bal, ccy)}
        </div>`;
      });
      chips += '</div>';
    }
  }

  const isAccounting = perspective === 'Accounting';
  const renderItem = (label, amount, types = []) => `
    <div style="flex: 1; min-width: 0; background: rgba(255,255,255,0.1); padding: 10px; border-radius: 10px; cursor: pointer"
         onclick="navigateWithFilter('${esc(cardId)}', {transactionTypes:${JSON.stringify(types).replace(/"/g, '&quot;')}})">
      <div style="font-size: 0.65rem; font-weight: 700; color: rgba(255,255,255,0.6); text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 4px">${label}</div>
      <div style="font-weight: 700; font-size: 0.95rem; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">
        ${fmt(amount, cur)}
      </div>
    </div>
  `;

  let flowGrid = '';
  if (isAccounting) {
    flowGrid = `
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 16px">
        ${renderItem('INCOME', d.income, ['INCOME'])}
        ${renderItem('EXPENSE', d.expense, ['EXPENSE'])}
        ${renderItem('TRF IN', d.transferIn, ['TRANSFER_INFLOW'])}
        ${renderItem('TRF OUT', d.transferOut, ['TRANSFER_OUTFLOW'])}
      </div>
    `;
  } else {
    flowGrid = `
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 16px">
        ${renderItem('INFLOW', (d.income || 0) + (d.transferIn || 0), ['INCOME', 'TRANSFER_INFLOW'])}
        ${renderItem('OUTFLOW', (d.expense || 0) + (d.transferOut || 0), ['EXPENSE', 'TRANSFER_OUTFLOW'])}
      </div>
    `;
  }

  return `
    <div class="account-hero" style="background: ${bg}; margin: -20px; padding: 24px; border-radius: var(--radius); color: #fff; position: relative; overflow: hidden">
      <div style="position: absolute; top: -20px; right: -20px; font-size: 8rem; opacity: 0.1; pointer-events: none">💳</div>

      <div style="position: relative; z-index: 1">
        <div style="font-size: 0.75rem; font-weight: 700; opacity: 0.7; text-transform: uppercase; letter-spacing: 0.1em; margin-bottom: 4px">
          ${isAccounting ? 'TOTAL BALANCE' : 'NET CASH FLOW'}
        </div>
        <div style="font-size: 2.2rem; font-weight: 800; letter-spacing: -0.02em; margin-bottom: 2px; cursor: pointer"
             onclick="navigateWithFilter('${esc(cardId)}')">
          ${fmt(d.balance, cur)}
        </div>
        ${d.currency !== cachedSettings.baseCurrency ? `<div style="font-size: 0.9rem; opacity: 0.8; font-weight: 500">≈ ${fmt(d.balanceInBase, cachedSettings.baseCurrency)}</div>` : ''}

        ${flowGrid}
        ${chips}
      </div>
    </div>
    <div style="height: 20px"></div>
  `;
}
